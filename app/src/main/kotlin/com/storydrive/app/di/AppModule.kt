package com.storydrive.app.di

import android.content.Context
import com.storydrive.app.BuildConfig
import com.storydrive.core.vehicle.VehicleGateway
import com.storydrive.core.vehicle.fake.FakeVehicleGateway
import com.storydrive.core.vehicle.pleos.PleosVehicleGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** 실제 차량 ↔ 주행 재생 전환 지점. 나머지 코드는 VehicleGateway 만 안다. */
    @Provides
    @Singleton
    fun vehicleGateway(@ApplicationContext context: Context): VehicleGateway =
        if (BuildConfig.USE_FAKE_VEHICLE) FakeVehicleGateway() else PleosVehicleGateway(context)
}
