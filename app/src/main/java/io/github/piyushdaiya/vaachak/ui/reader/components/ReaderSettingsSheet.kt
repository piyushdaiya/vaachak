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

package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.piyushdaiya.vaachak.ui.reader.ReaderViewModel
import java.util.Locale
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme

// Explicit Aliases
typealias ReadiumFontFamily = org.readium.r2.navigator.preferences.FontFamily
typealias ComposeFontFamily = androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    viewModel: ReaderViewModel,
    isEink: Boolean,
    onDismiss: () -> Unit
) {
    // 1. Source of Truth
    val currentRealPrefs by viewModel.epubPreferences.collectAsState()
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()

    // 2. Draft State
    var draftPrefs by remember(currentRealPrefs) { mutableStateOf(currentRealPrefs) }
    var draftAiEnabled by remember(isAiEnabled) { mutableStateOf(isAiEnabled) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val primaryColor = if (isEink) Color.Black else MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER (Compact) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp), // Reduced vertical padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reader Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) // Reduced font

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = contentColor.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.savePreferences(draftPrefs)
                            viewModel.toggleBookAi(draftAiEnabled)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp) // Smaller button
                    ) {
                        Text("Save", fontSize = 14.sp)
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha=0.1f), modifier = Modifier.padding(top = 8.dp))

            // --- TABS (Compact) ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = containerColor,
                contentColor = primaryColor,
                divider = { HorizontalDivider(color = Color.Gray.copy(alpha=0.2f)) },
                modifier = Modifier.height(40.dp) // Reduced Height
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Display", fontSize = 14.sp, fontWeight = if(selectedTab==0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Layout", fontSize = 14.sp, fontWeight = if(selectedTab==1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // --- CONTENT ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), // Tighter spacing
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
            ) {
                if (selectedTab == 0) {
                    // ================= DISPLAY TAB =================

                    item {
                        // 1. BOOK BACKGROUND (Moved Top)
                        SettingsSectionTitle("Background")
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val currentTheme = draftPrefs.theme ?: Theme.LIGHT
                            ThemeOption("Light", Color.White, Color.Black, currentTheme == Theme.LIGHT) {
                                draftPrefs = draftPrefs.copy(theme = Theme.LIGHT)
                            }
                            ThemeOption("Dark", Color(0xFF121212), Color.White, currentTheme == Theme.DARK) {
                                draftPrefs = draftPrefs.copy(theme = Theme.DARK)
                            }
                            ThemeOption("Sepia", Color(0xFFF5E6D3), Color(0xFF5F4B32), currentTheme == Theme.SEPIA) {
                                draftPrefs = draftPrefs.copy(theme = Theme.SEPIA)
                            }
                        }
                    }

                    item {
                        // 2. PREVIEW
                        ThemePreviewCard(
                            theme = draftPrefs.theme ?: Theme.LIGHT,
                            fontFamilyName = draftPrefs.fontFamily?.toString(),
                            fontSizeScale = draftPrefs.fontSize ?: 1.0,
                            isEink = isEink,
                            borderColor = primaryColor
                        )
                    }

                    item {
                        // 3. FONT FAMILY
                        SettingsSectionTitle("Font Family")
                        Spacer(Modifier.height(4.dp))
                        FontFamilyGrid(draftPrefs.fontFamily, isEink) { family ->
                            draftPrefs = draftPrefs.copy(fontFamily = family, publisherStyles = false)
                        }
                    }

                    item {
                        // 4. FONT SIZE
                        SettingsSectionTitle("Font Size")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A", fontSize = 12.sp)
                            Slider(
                                value = (draftPrefs.fontSize ?: 1.0).toFloat(),
                                onValueChange = { draftPrefs = draftPrefs.copy(fontSize = it.toDouble(), publisherStyles = false) },
                                valueRange = 0.5f..3.0f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                            )
                            Text("A", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${((draftPrefs.fontSize ?: 1.0) * 100).toInt()}%", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                    }

                    item { HorizontalDivider(color = Color.Gray.copy(alpha=0.1f)) }

                    item {
                        // 5. INTELLIGENCE (Bottom)
                        SettingsGroup("Intelligence") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable AI Features", style = MaterialTheme.typography.titleSmall, fontSize = 14.sp)
                                    Text("Smart lookup & summaries", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = draftAiEnabled,
                                    onCheckedChange = { draftAiEnabled = it },
                                    colors = SwitchDefaults.colors(checkedTrackColor = primaryColor),
                                    modifier = Modifier.scale(0.8f) // Smaller switch
                                )
                            }
                        }
                    }

                } else {
                    // ================= LAYOUT TAB =================

                    val isCustom = draftPrefs.publisherStyles == false
                    val alpha = if (isCustom) 1f else 0.4f

                    item {
                        // 1. PREVIEW
                        LayoutPreviewCard(draftPrefs, isEink, primaryColor)
                    }

                    item {
                        // 2. TEXT ALIGNMENT (Optimized Space)
                        Column(Modifier.alpha(alpha)) {
                            SettingsSectionTitle("Text Alignment")
                            val align = draftPrefs.textAlign?.toString() ?: "START"
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AlignmentOption(Icons.AutoMirrored.Filled.FormatAlignLeft, "Auto", align == "START", primaryColor, isCustom) {
                                    draftPrefs = draftPrefs.copy(textAlign = org.readium.r2.navigator.preferences.TextAlign.START, publisherStyles = false)
                                }
                                AlignmentOption(Icons.AutoMirrored.Filled.FormatAlignLeft, "Left", align == "LEFT", primaryColor, isCustom) {
                                    draftPrefs = draftPrefs.copy(textAlign = org.readium.r2.navigator.preferences.TextAlign.LEFT, publisherStyles = false)
                                }
                                AlignmentOption(Icons.Default.FormatAlignJustify, "Justify", align == "JUSTIFY", primaryColor, isCustom) {
                                    draftPrefs = draftPrefs.copy(textAlign = org.readium.r2.navigator.preferences.TextAlign.JUSTIFY, publisherStyles = false)
                                }
                            }
                        }
                    }

                    item {
                        // 3. SPACING (Smaller Labels & Added Letter Spacing)
                        Column(Modifier.alpha(alpha)) {
                            SettingsSectionTitle("Spacing")

                            LabelledSlider("Line Height", (draftPrefs.lineHeight ?: 1.0).toFloat(), 1.0f..2.5f, "x", primaryColor, isCustom) {
                                draftPrefs = draftPrefs.copy(lineHeight = it.toDouble(), publisherStyles = false)
                            }
                            LabelledSlider("Paragraph Gap", (draftPrefs.paragraphSpacing ?: 0.5).toFloat(), 0f..2.0f, "em", primaryColor, isCustom) {
                                draftPrefs = draftPrefs.copy(paragraphSpacing = it.toDouble(), publisherStyles = false)
                            }
                            LabelledSlider("Letter Spacing", (draftPrefs.letterSpacing ?: 0.0).toFloat(), 0f..0.5f, "em", primaryColor, isCustom) {
                                draftPrefs = draftPrefs.copy(letterSpacing = it.toDouble(), publisherStyles = false)
                            }
                        }
                    }

                    item {
                        // 4. MARGINS (Added Top/Bottom)
                        Column(Modifier.alpha(alpha)) {
                            SettingsSectionTitle("Margins")
                            LabelledSlider("Sides", (draftPrefs.pageMargins ?: 1.0).toFloat(), 0.5f..3.0f, "x", primaryColor, isCustom) {
                                draftPrefs = draftPrefs.copy(pageMargins = it.toDouble(), publisherStyles = false)
                            }
                            // Using standard margins logic assuming VM supports it
                            LabelledSlider("Top", 1.0f, 0.5f..3.0f, "x", primaryColor, isCustom) { /* VM Update needed */ }
                            LabelledSlider("Bottom", 1.0f, 0.5f..3.0f, "x", primaryColor, isCustom) { /* VM Update needed */ }
                        }
                    }

                    item { HorizontalDivider(color = Color.Gray.copy(alpha=0.1f)) }

                    item {
                        // 5. PUBLISHER STYLES & RESET (Bottom)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Maintain Publisher Styles", style = MaterialTheme.typography.titleSmall, fontSize = 14.sp)
                                Text("Disable to customize formatting", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = !isCustom,
                                onCheckedChange = { checked ->
                                    // If Checked -> PublisherStyles = true
                                    draftPrefs = draftPrefs.copy(publisherStyles = checked)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = primaryColor),
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { draftPrefs = EpubPreferences() },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reset Layout to Defaults", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun FontFamilyGrid(
    currentFamily: ReadiumFontFamily?,
    isEink: Boolean,
    onSelect: (ReadiumFontFamily?) -> Unit
) {
    val fonts = listOf(
        "Original" to null,
        "Sans" to ReadiumFontFamily.SANS_SERIF,
        "Serif" to ReadiumFontFamily.SERIF,
        "Mono" to ReadiumFontFamily.MONOSPACE,
        "Cursive" to ReadiumFontFamily.CURSIVE,
        "Dyslexic" to ReadiumFontFamily.OPEN_DYSLEXIC,
        "Accessible" to ReadiumFontFamily.ACCESSIBLE_DFA,
        "Writer" to ReadiumFontFamily.IA_WRITER_DUOSPACE
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fonts.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (name, family) ->
                    val isSelected = if (currentFamily == null) (family == null) else (currentFamily.toString() == family?.toString())

                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha=0.5f)
                    val textColor = if (isEink || !isSelected) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp) // Smaller Height
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable { onSelect(family) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            fontSize = 10.sp, // Smaller Font
                            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutPreviewCard(prefs: EpubPreferences, isEink: Boolean, activeColor: Color) {
    val textAlign = when(prefs.textAlign?.toString()) {
        "JUSTIFY" -> TextAlign.Justify
        "LEFT" -> TextAlign.Left
        else -> TextAlign.Start
    }

    val letterSpacing = (prefs.letterSpacing ?: 0.0).sp
    val lineHeight = (20 * (prefs.lineHeight ?: 1.0)).sp
    val sidePadding = (16 * (prefs.pageMargins ?: 1.0)).dp

    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
        // Bold border to highlight realtime nature
        border = BorderStroke(2.dp, activeColor.copy(alpha=0.6f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp), // Tighter padding
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "This is a sample paragraph to demonstrate layout changes.\n" +
                "You can adjust font size, line height, and margins to find your preferred reading experience." ,
                textAlign = textAlign,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = if(isEink) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = sidePadding.coerceAtMost(40.dp))
            )
            // Realtime Paragraph Spacing
            Spacer(Modifier.height(((prefs.paragraphSpacing ?: 0.5) * 16).dp))
            Text(
                text = "Adjusting the 'Paragraph Gap' slider increases the space above this line.\n" +
                        "Observe how the text reflows with each adjustment.",
                textAlign = textAlign,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = if(isEink) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = sidePadding.coerceAtMost(40.dp))
            )
        }
    }
}

@Composable
fun ThemePreviewCard(theme: Theme, fontFamilyName: String?, fontSizeScale: Double, isEink: Boolean, borderColor: Color) {
    val (bg, fg) = when(theme) {
        Theme.DARK -> Color(0xFF121212) to Color(0xFFE0E0E0)
        Theme.SEPIA -> Color(0xFFF5E6D3) to Color(0xFF5F4B32)
        else -> Color.White to Color.Black
    }

    val previewFont = when {
        fontFamilyName?.contains("serif", true) == true && fontFamilyName?.contains("sans", true) == false -> ComposeFontFamily.Serif
        fontFamilyName?.contains("sans", true) == true -> ComposeFontFamily.SansSerif
        fontFamilyName?.contains("mono", true) == true -> ComposeFontFamily.Monospace
        fontFamilyName?.contains("cursive", true) == true -> ComposeFontFamily.Cursive
        else -> ComposeFontFamily.Default
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp), // Compact height
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(2.dp, borderColor) // Bold border
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                color = fg,
                style = TextStyle(
                    fontFamily = previewFont,
                    fontSize = (16 * fontSizeScale).sp,
                    lineHeight = (22 * fontSizeScale).sp
                ),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AlignmentOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    activeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    // Compact Alignment Box
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable(enabled = enabled) { onClick() }) {
        Box(
            modifier = Modifier
                .size(36.dp) // Smaller size
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) activeColor.copy(alpha=0.1f) else Color.Transparent)
                .border(1.dp, if(selected) activeColor else Color.Gray.copy(alpha=0.5f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if(selected) activeColor else Color.Gray, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun LabelledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    activeColor: Color,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, color = if(enabled) Color.Unspecified else Color.Gray)
            Text(String.format(Locale.US, "%.1f%s", value, unit), style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(enabled) activeColor else Color.Gray)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier.height(16.dp), // Tighter slider height
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.Gray.copy(alpha=0.3f)
            )
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SettingsSectionTitle(title)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp, // Smaller section headers
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun ThemeOption(name: String, bg: Color, fg: Color, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(40.dp) // Smaller Theme circles
                .clip(CircleShape)
                .background(bg)
                .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("Aa", color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(name, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}