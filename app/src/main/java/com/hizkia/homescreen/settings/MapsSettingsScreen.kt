package com.hizkia.homescreen.settings

import androidx.compose.ui.res.stringResource
import com.hizkia.homescreen.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizkia.homescreen.ui.Palette
import com.hizkia.homescreen.ui.clickableNoRipple

@Composable
fun MapsSettingsScreen(
    home: String,
    office: String,
    onSave: (home: String, office: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var homeText by remember { mutableStateOf(home) }
    var officeText by remember { mutableStateOf(office) }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.back_settings), color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.clickableNoRipple(onBack))
        Text(stringResource(R.string.maps_shortcuts), color = Palette.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.maps_settings_intro),
            color = Palette.Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

        AddressField(stringResource(R.string.home_address), homeText) { homeText = it }
        AddressField(stringResource(R.string.office_address), officeText) { officeText = it }

        Column(Modifier.weight(1f)) {}

        Button(
            onClick = { onSave(homeText, officeText) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Text, contentColor = Palette.Bg),
        ) {
            Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddressField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Palette.Muted, fontSize = 13.sp)
        TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(stringResource(R.string.address_hint), color = Palette.Muted) },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface,
                focusedTextColor = Palette.Text,
                unfocusedTextColor = Palette.Text,
                cursorColor = Palette.Mint,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
