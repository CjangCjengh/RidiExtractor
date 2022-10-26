package com.example.ridiextractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ridiextractor.ui.theme.RidiExtractorTheme
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask for permission to read and write to external storage
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                // Permission granted
                setContent {
                    MainInterface(this)
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                // Close the app
                finish()
            }
        }
        requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

data class Book(val id:String,val name: String,val checked: MutableState<Boolean>)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainInterface(context: Context?) {
    RidiExtractorTheme {
        Column {
            val selected = remember { mutableStateListOf<Book>() }
            // Title bar
            Surface(color = MaterialTheme.colors.primary, modifier = Modifier.fillMaxWidth()) {
                Row {
                    Text(
                        text = "RidiExtractor",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Create a dialog to fill in save path
                    val openDialog = remember { mutableStateOf(false) }
                    val appDirectory = File(Environment.getExternalStorageDirectory().absolutePath + "/RidiExtractor")
                    val storageDirectory = remember { mutableStateOf(appDirectory.absolutePath) }
                    if (!appDirectory.exists()) {
                        appDirectory.mkdir()
                    } else {
                        val settingFile = File("$appDirectory/settings.txt")
                        try {
                            storageDirectory.value = settingFile.readText()
                        }
                        catch (e:Exception) { }
                    }
                    if (openDialog.value) {
                        AlertDialog(
                            onDismissRequest = { openDialog.value = false },
                            title = { Text("Extract Books") },
                            text = {
                                Column {
                                    TextField(
                                        value = storageDirectory.value,
                                        onValueChange = { storageDirectory.value = it },
                                        label = { Text("Storage Directory") }
                                    )
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    // Make sure the directory exists
                                    val directory = File(storageDirectory.value)
                                    if(!directory.exists())
                                        directory.mkdir()
                                    // Save settings
                                    val settingFile = File("$appDirectory/settings.txt")
                                    try {
                                        settingFile.writeText(storageDirectory.value)
                                    }
                                    catch (e:Exception) { }
                                    // Extract books in a new thread
                                    val books = selected.toList()
                                    Thread {
                                        extractBooks(books, directory, context)
                                    }.start()
                                    // Close the dialog
                                    for (book in selected)
                                        book.checked.value = false
                                    selected.clear()
                                    openDialog.value = false
                                }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                    // Extract button
                    AnimatedVisibility(visible = selected.isNotEmpty(), modifier=Modifier.align(Alignment.CenterVertically)) {
                        Box(modifier = Modifier
                            .padding(5.dp)
                            .border(1.dp, MaterialTheme.colors.secondary, RoundedCornerShape(5.dp))
                            .align(Alignment.CenterVertically)
                            .clickable {
                                openDialog.value = true
                            }) {
                            Text(
                                text = "Extract (${selected.size})",
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                                    .padding(10.dp, 5.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
            // Book list
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                BookList(selected)
            }
        }
    }
}

@Composable
fun BookList(selected: MutableList<Book>) {
    val ridiDirectory = "${Environment.getDataDirectory()}/data/com.initialcoms.ridi/files"
    // List all book directories
    val books = File("$ridiDirectory/books").listFiles()
    if (books != null) {
        for (book in books){
            val bookId = book.name
            // Find book cover
            val cover = File("$ridiDirectory/covers/$bookId.png")
            // Find book info
            val infoFile = File("$book/extracted/OEBPS/content.opf")
            if (infoFile.exists()) {
                val info = infoFile.readText(Charsets.UTF_8)
                val title = info.substringAfter("<dc:title>").substringBefore("</dc:title>")
                val author = info.substringAfter("<dc:creator opf:role=\"aut\">").substringBefore("</dc:creator>")
                BookItem(bookId, cover, title, author, selected)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BookItem(id: String, cover: File, title: String, author: String, selected: MutableList<Book>) {
    val checked = remember { mutableStateOf(false) }
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(5.dp)
        .heightIn(max = 100.dp)
        .clickable {
            checked.value = !checked.value
            if (checked.value) {
                selected.add(Book(id, title, checked))
            } else {
                selected.remove(Book(id, title, checked))
            }
        }) {
        // Check box
        AnimatedVisibility(
            visible = checked.value,
            modifier = Modifier
                .align(Alignment.CenterVertically),
        ) {
            Checkbox(
                checked = checked.value,
                onCheckedChange = { },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(5.dp)
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        // Book cover
        Image(
            bitmap = BitmapFactory.decodeFile(cover.absolutePath)?.asImageBitmap() ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap(),
            contentDescription = "Book cover",
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .size(70.dp, 100.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        // Book info
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = author, style = MaterialTheme.typography.body1, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
        }
    }
    Divider()
}

@WorkerThread
fun extractBooks(books: List<Book>, directory: File, context: Context?) {
    val ridiDirectory = "${Environment.getDataDirectory()}/data/com.initialcoms.ridi"
    // Find device id
    val deviceId: String
    try {
        val ridiPreferences = File("$ridiDirectory/shared_prefs/com.initialcoms.ridi_preferences.xml").readText()
        deviceId = ridiPreferences.substringAfter("<string name=\"uuid\">").substringBefore("</string>")
    }
    catch (e: Exception) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Failed to find device id", Toast.LENGTH_LONG).show()
        }
        return
    }
    // Extract books
    for((index, book) in books.withIndex()) {
        try {
            // Copy book directory
            val bookDirectory = File("$ridiDirectory/files/books/${book.id}")
            val contentDirectory = File("$bookDirectory/extracted")
            val targetDirectory = File("$directory/${book.id}")
            if (targetDirectory.exists())
                targetDirectory.deleteRecursively()
            contentDirectory.copyRecursively(targetDirectory, true)
            // Get key
            val key = RidiDecrypter.generateKey(deviceId, File("$bookDirectory/${book.id}.dat"))
            // Decrypt html files
            val htmlFiles =
                File("$targetDirectory/OEBPS/Text").listFiles()?.filter { it.extension == "xhtml" || it.extension == "html" }
            if (htmlFiles != null) {
                for (file in htmlFiles) {
                    var text = RidiDecrypter.decryptHtml(file, key)
                    text = Regex("([\\s\\S])\\1+$").replace(text, "")
                    file.writeText(text)
                }
            }
            // Delete redundant files
            File("$targetDirectory/OEBPS/Styles/style.css.ridibackup").delete()
            // Zip the book
            val zipFile = File("$directory/${book.name}.epub")
            if (zipFile.exists())
                zipFile.delete()
            ZipUtil.pack(targetDirectory, zipFile)
            // Delete the book directory
            targetDirectory.deleteRecursively()
            // Toast message
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "${book.name} extracted (${index + 1}/${books.size})", Toast.LENGTH_SHORT).show()
            }
        }
        catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to extract ${book.name}", Toast.LENGTH_LONG).show()
            }
        }
    }
}