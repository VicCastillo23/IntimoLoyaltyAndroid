package com.intimocoffee.loyalty.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intimocoffee.loyalty.ui.theme.IntimoColors

val INTIMO_GENDER_OPTIONS = listOf(
    "" to "Sin especificar",
    "MALE" to "Masculino",
    "FEMALE" to "Femenino",
    "OTHER" to "Otro",
    "PREFER_NOT_SAY" to "Prefiero no decir",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntimoGenderField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Género",
) {
    val selectedLabel = INTIMO_GENDER_OPTIONS.find { it.first == value }?.second ?: "Sin especificar"
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            color = IntimoColors.SubtleText,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IntimoColors.Espresso,
                    unfocusedBorderColor = IntimoColors.BorderMuted,
                    focusedTextColor = IntimoColors.Cream,
                    unfocusedTextColor = IntimoColors.Cream,
                    cursorColor = IntimoColors.Espresso,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                INTIMO_GENDER_OPTIONS.forEach { (optionValue, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onValueChange(optionValue)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
