package edu.gvsu.cis357.docscanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainScreen(navController: NavController, modifier: Modifier = Modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // draw lines for main screen design
        drawLine(
            start = Offset(x = 60f, y = 0f),
            end = Offset(x = 60f, y = canvasHeight),
            color = Color.Black,
            strokeWidth = 5F
        )
        drawLine(
            start = Offset(x = canvasWidth - 60f, y = 0f),
            end = Offset(x = canvasWidth - 60f, y = canvasHeight),
            color = Color.Black,
            strokeWidth = 5F
        )
        drawLine(
            start = Offset(x = 0f, y = 110f),
            end = Offset(x = canvasWidth, y = 110f),
            color = Color.Black,
            strokeWidth = 5F
        )
        drawLine(
            start = Offset(x = 0f, y = canvasHeight - 110f),
            end = Offset(x = canvasWidth, y = canvasHeight - 110f),
            color = Color.Black,
            strokeWidth = 5F
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DocScanner",
                fontSize = 36.sp,
                modifier = modifier
                    .padding(vertical = 36.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.baseline_document_scanner_24),
                contentDescription = "Document Scanner Icon",
                modifier = Modifier
                    .size(500.dp)
                    .padding(36.dp)
            )
        }
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Button(
                    onClick = {
                        navController.navigate("scans")
                    },
                    modifier = Modifier
                        .padding(vertical = 100.dp, horizontal = 55.dp)
                        .border(2.dp, Color.Black),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = "SCANS",
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                }
                Button(
                    onClick = {
                        navController.navigate("camera")
                    },
                    modifier = Modifier
                        .padding(vertical = 100.dp, horizontal = 55.dp)
                        .border(2.dp, Color.Black),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                ) {
                    Text(
                        text = "NEW",
                        color = Color.Black,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}