package com.routeleague.app.di

import android.content.Context
import androidx.room.Room
import com.routeleague.app.BuildConfig
import com.routeleague.core.data.ActiveRouteStore
import com.routeleague.core.data.RecordRepository
import com.routeleague.core.data.local.RecordDatabase
import com.routeleague.core.data.remote.OfflineSyncApi
import com.routeleague.core.data.remote.RemoteSyncApi
import com.routeleague.core.vehicle.VehicleGateway
import com.routeleague.core.vehicle.fake.FakeVehicleGateway
import com.routeleague.core.vehicle.pleos.PleosVehicleGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** 실제 차량 ↔ 시나리오 재생 전환 지점. 나머지 코드는 VehicleGateway 만 안다. */
    @Provides
    @Singleton
    fun vehicleGateway(@ApplicationContext context: Context): VehicleGateway =
        if (BuildConfig.USE_FAKE_VEHICLE) FakeVehicleGateway() else PleosVehicleGateway(context)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): RecordDatabase =
        Room.databaseBuilder(context, RecordDatabase::class.java, "records.db").build()

    @Provides
    @Singleton
    fun recordRepository(db: RecordDatabase): RecordRepository = RecordRepository(db.recordDao())

    @Provides
    @Singleton
    fun activeRouteStore(): ActiveRouteStore = ActiveRouteStore()

    /** TODO: Supabase 구현으로 교체 (backend/README.md). */
    @Provides
    @Singleton
    fun remoteSyncApi(): RemoteSyncApi = OfflineSyncApi()
}
