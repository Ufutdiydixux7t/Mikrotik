package com.example.mikrotik.di

import com.example.mikrotik.core.captive.CaptivePortalServer
import com.example.mikrotik.core.hotspot.ClientDiscoveryManager
import com.example.mikrotik.core.hotspot.HotspotController
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.core.network.NetworkController
import com.example.mikrotik.core.root.RootDiagnostics
import com.example.mikrotik.core.root.RootShellManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RootModule {

    @Provides
    @Singleton
    fun provideRootShellManager(): RootShellManager {
        return RootShellManager()
    }

    @Provides
    @Singleton
    fun provideRootDiagnostics(rootShellManager: RootShellManager): RootDiagnostics {
        return RootDiagnostics(rootShellManager)
    }

    @Provides
    @Singleton
    fun provideHotspotController(
        rootShellManager: RootShellManager,
        iptablesManager: IptablesManager,
        captivePortalServer: CaptivePortalServer
    ): HotspotController {
        return HotspotController(rootShellManager, iptablesManager, captivePortalServer)
    }

    @Provides
    @Singleton
    fun provideClientDiscoveryManager(rootShellManager: RootShellManager): ClientDiscoveryManager {
        return ClientDiscoveryManager(rootShellManager)
    }

    @Provides
    @Singleton
    fun provideNetworkController(rootShellManager: RootShellManager): NetworkController {
        return NetworkController(rootShellManager)
    }
}
