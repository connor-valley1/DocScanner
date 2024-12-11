package edu.gvsu.cis357.docscanner

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScansScreen(navController: NavController) {
    val context = LocalContext.current
    val scansFolder = getScansFolder(context)
    val files = remember { scansFolder.listFiles()?.filter { it.extension == "pdf" } ?: emptyList() }
    val fileList = remember { mutableStateListOf<File>().apply { addAll(files) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Scanned Files",
                            fontSize = 36.sp,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(bottom = 16.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    modifier = Modifier
                        .border(2.dp, Color.Black),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    onClick = {
                        navController.navigate("main")
                    }
                ) {
                    Text(
                        text = "Back",
                        color = Color.Black,
                        fontSize = 24.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        if (fileList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No scans available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(fileList) { file ->
                    FileCard(
                        file = file,
                        onDelete = { fileToDelete ->
                            fileToDelete.delete()
                            fileList.remove(fileToDelete)
                            Toast.makeText(context, "File deleted: ${fileToDelete.name}", Toast.LENGTH_SHORT).show()
                        },
                        onView = { fileToView ->
                            openFile(context, fileToView)
                        }
                    )
                }
            }
        }

    }
}

@Composable
fun FileCard(file: File, onDelete: (File) -> Unit, onView: (File) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Size: ${file.length() / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Button(
                onClick = { onView(file) },
                modifier = Modifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "View",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
            IconButton(onClick = { onDelete(file) }) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_24),
                    contentDescription = "Delete File"
                )
            }
        }
    }
}

// helper function to get or create the scans folder
fun getScansFolder(context: Context): File {
    // set directory for scans
    val scansFolder = File(context.getExternalFilesDir(null), "scans")
    // check if directory exists, create it if not
    if (!scansFolder.exists()) {
        scansFolder.mkdirs()
    }
    // return the location of the scans folder
    return scansFolder
}

// helper function to open the PDF file for viewing
fun openFile(context: Context, file: File) {
    try {
        // get the URI for the file being opened
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        // create an intent to open the PDF file
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // start the activity to open the PDF file
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        // handle errors with opening the PDF file
        Toast.makeText(context, "No application available to open PDF", Toast.LENGTH_SHORT).show()
    }
}
