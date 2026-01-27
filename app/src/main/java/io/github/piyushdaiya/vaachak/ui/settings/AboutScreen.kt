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
            Text("Version 1.2.0 (Stable)", fontSize = 14.sp, color = Color.Gray)
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

            // READIUM LICENSE (MANDATORY)
            LicenseItem(
                library = "Readium Kotlin Toolkit",
                license = """
                    Copyright (c) 2018-2024, EDRLab. All rights reserved.
                    
                    Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
                    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
                    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
                    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
                    
                    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES... (See full BSD-3 Clause)
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

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Built with ❤️ by Piyush Daiya",
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

