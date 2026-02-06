/*
 *  Copyright (c) 2026 Piyush Daiya
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 */

package io.github.piyushdaiya.vaachak.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Vaachak") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. APP INFO
            Text("Vaachak", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

            // --- UPDATED: Version 2.0.1---
            Text("Version 2.0.1 (Stable)", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "An AI-powered E-Reader built for the modern age. Read, highlight, and explore your books with the power of Gemini and Cloudflare.",
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // 2. OPEN SOURCE LICENSES
            Text(
                "Open Source Licenses",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            // --- VAACHAK LICENSE (MIT) ---
            LicenseItem(
                library = "Vaachak (This Software)",
                license = """
                    Copyright (c) 2026 Piyush Daiya.
                    Licensed under the MIT License.
                    
                    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files...
                """.trimIndent()
            )

            // READIUM LICENSE (MANDATORY BSD-3)
            LicenseItem(
                library = "Readium Kotlin Toolkit",
                license = """
                    Copyright (c) 2018-2024, EDRLab. All rights reserved.
                    Licensed under the BSD 3-Clause License.
                    
                    Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met...
                """.trimIndent()
            )

            // OTHER LIBRARIES
            LicenseItem(
                library = "Android Jetpack & Material 3",
                license = "Licensed under the Apache License, Version 2.0."
            )

            LicenseItem(
                library = "Google Generative AI (Gemini)",
                license = "Licensed under the Apache License, Version 2.0."
            )

            LicenseItem(
                library = "Retrofit & OkHttp",
                license = "Copyright 2013 Square, Inc. Licensed under the Apache License, Version 2.0."
            )

            LicenseItem(
                library = "Hilt (Dagger)",
                license = "Copyright (C) 2020 The Dagger Authors. Licensed under the Apache License, Version 2.0."
            )

            // --- NEW: Coil & Room ---
            LicenseItem(
                library = "Coil (Image Loading)",
                license = "Copyright 2023 Coil Contributors. Licensed under the Apache License, Version 2.0."
            )

            LicenseItem(
                library = "Room Database",
                license = "Licensed under the Apache License, Version 2.0."
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Built with Google Gemini by Piyush Daiya",
                fontSize = 12.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LicenseItem(library: String, license: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(library, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(license, fontSize = 12.sp, color = Color.Gray)
    }
}