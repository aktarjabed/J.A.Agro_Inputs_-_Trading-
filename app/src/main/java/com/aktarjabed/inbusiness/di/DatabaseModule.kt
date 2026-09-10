package com.aktarjabed.inbusiness.di

import android.content.Context
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.security.KeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKeyProvider(
        @ApplicationContext context: Context
    ): KeyProvider = KeyProvider(context)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyProvider: KeyProvider
    ): AppDatabase = AppDatabase.getDatabase(context, keyProvider)

    @Provides
    @Singleton
    fun provideBusinessDao(
        database: AppDatabase
    ): BusinessDao = database.businessDao()

    @Provides
    @Singleton
    fun provideInvoiceDao(
        database: AppDatabase
    ): InvoiceDao = database.invoiceDao()

    @Provides
    @Singleton
    fun provideUserQuotaDao(
        database: AppDatabase
    ): UserQuotaDao = database.userQuotaDao()

    @Provides
    @Singleton
    fun provideProductDao(
        database: AppDatabase
    ): ProductDao = database.productDao()
}