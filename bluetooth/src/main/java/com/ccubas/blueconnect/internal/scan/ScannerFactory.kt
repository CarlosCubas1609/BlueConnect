package com.ccubas.blueconnect.internal.scan

import android.content.Context

/**
 * Builds the list of [IScanSource]s to run for one scan session.
 *
 * Sources run in parallel via `flatMapMerge`, so the order of the list is informative only.
 * Adding a new transport (e.g. a Wi-Fi Direct probe) is just appending a new
 * [IScanSource] implementation here. Tests can swap in a fake factory.
 */
internal interface IScannerFactory {

    /**
     * @param demoMode when true, only the synthetic [DemoScanSource] is returned.
     * @param durationMs duration the coordinator will keep the scan alive; some sources
     *                   (e.g. [DemoScanSource]) use it to pace their output.
     */
    fun createScanSources(demoMode: Boolean, durationMs: Long): List<IScanSource>
}

internal class ScannerFactory(private val context: Context) : IScannerFactory {

    override fun createScanSources(demoMode: Boolean, durationMs: Long): List<IScanSource> =
        if (demoMode) {
            listOf(DemoScanSource(context, durationMs))
        } else {
            listOf(
                BondedDevicesScanSource(context),
                BleScanSource(context),
                ClassicDiscoveryScanSource(context),
            )
        }
}
