package com.example.thirdlabkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlin.math.*
import com.example.thirdlabkotlin.ui.theme.ThirdLabKotlinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThirdLabKotlinTheme {
                calculatProfitFromSolarPowerPlants()
            }
        }
    }
}

@Preview
@Composable
fun calculatProfitFromSolarPowerPlants() {
    var Pc by remember { mutableStateOf("") }
    var deviation by remember { mutableStateOf("") }
    var forecastError by remember { mutableStateOf("") }
    var energyPrice by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(mapOf<String, Double>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Калькулятор розрахунку прибутку від сонячних електростанцій")

        TextField(
            value = Pc,
            onValueChange = { Pc = it },
            label = { Text(text = "Середньодобова потужність (МВт)") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = deviation,
            onValueChange = { deviation = it },
            label = { Text(text = "Середньоквадратичне відхилення (МВт)") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = forecastError,
            onValueChange = { forecastError = it },
            label = { Text(text = "Похибка прогнозу (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = energyPrice,
            onValueChange = { energyPrice = it },
            label = { Text(text = "Вартість електроенергії (грн/кВт*год)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                result = calculateProfit(
                    forecastError = forecastError.toDouble(),
                    deviation = deviation.toDouble(),
                    Pc = Pc.toDouble()
                )
            },
            enabled = forecastError.isNotEmpty() && deviation.isNotEmpty() && Pc.isNotEmpty() && energyPrice.isNotEmpty()
        ) {
            Text(text = "Розрахувати")
        }

        result.let {map ->
            if (map.isNotEmpty()) {
                val energyFractionWithoutImbalance = map["energyFractionWithoutImbalance"]
                val energyAmount = map["energyAmount"]
                val energyFractionImbalance = map["energyFractionImbalance"]
                val energyAmountImbalance = map["energyAmountImbalance"]
                val profit = energyAmount?.times(energyPrice.toDouble())
                val loss = energyAmountImbalance?.times(energyPrice.toDouble())

                Text(text = "Частка енергії, що генерується без небалансів: ${String.format("%.2f", energyFractionWithoutImbalance)}%")
                Text(text = "${String.format("%.2f", energyFractionWithoutImbalance)}% = ${String.format("%.2f", energyAmount)} МВт*год")
                if (energyAmount != null) Text(text = "Прибуток: ${String.format("%.2f", (profit?.toDouble()))} грн")
                else Text(text = "Прибуток: помилка у розрахунку")

                Text(text = "Частка енергії небалансу: ${String.format("%.2f", energyFractionImbalance)}%")
                Text(text = "${String.format("%.2f", energyFractionImbalance)}% = ${String.format("%.2f", energyAmountImbalance)} МВт*год")
                if (energyAmountImbalance != null) Text(text = "Штраф: ${String.format("%.2f", loss?.toDouble())} грн")
                else Text(text = "Прибуток: помилка у розрахунку")
                Text(text = "Загальний прибуток: ${String.format("%.2f", (profit?.minus(loss!!)))} грн")
            }
        }

    }

}

fun calculateProfit(forecastError: Double, deviation: Double, Pc: Double): Map<String, Double> {
    val energyFractionWithoutImbalance = getShareOfEnergyWithoutImbalances(forecastError, deviation, Pc)
    val energyAmount = (Pc * 24.0 * energyFractionWithoutImbalance) / 100.0
    val energyFractionImbalance = (100.0 - energyFractionWithoutImbalance)
    val energyAmountImbalance = ((Pc * 24.0 * (1.0 - energyFractionImbalance)) / 100.0) * -1

    return mapOf(
        "energyFractionWithoutImbalance" to energyFractionWithoutImbalance,
        "energyAmount" to energyAmount,
        "energyFractionImbalance" to energyFractionImbalance,
        "energyAmountImbalance" to energyAmountImbalance
    )
}

fun getShareOfEnergyWithoutImbalances(forecastError: Double, deviation: Double, Pc: Double): Double {
    val upperValue = Pc + (Pc * (forecastError / 100))
    val lowerValue = Pc - (Pc * (forecastError / 100))

    val steps = 1000
    val stepSize = (upperValue - lowerValue) / steps
    var integral = 0.0

    for (i in 0 until steps) {
        val x1 = lowerValue + i * stepSize
        val x2 = lowerValue + (i + 1) * stepSize
        integral += (calculatePd(deviation, x1, Pc) + calculatePd(deviation, x2, Pc)) * stepSize / 2
    }

    return integral * 100
}

fun calculatePd(deviation: Double, p: Double, Pc: Double): Double {
    return (1 / (deviation * sqrt(2 * PI))) * exp(-((p - Pc).pow(2)) / (2 * deviation.pow(2)))
}