package com.schedule.shift.di

import android.content.Context
import androidx.room.Room
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.schedule.shift.data.db.ScheduleWeekDao
import com.schedule.shift.data.db.ShiftDatabase
import com.schedule.shift.data.reporter.NoOpFailedImageReporter
import com.schedule.shift.data.repository.ScheduleRepositoryImpl
import com.schedule.shift.domain.ocr.OcrEngine
import com.schedule.shift.domain.parser.ScheduleParser
import com.schedule.shift.domain.reporter.FailedImageReporter
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
import com.schedule.shift.ocr.MlKitOcrEngine
import com.schedule.shift.ocr.ScheduleParserImpl
import com.schedule.shift.ocr.Stage1Validator
import com.schedule.shift.ui.di.TodayDate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @TodayDate
    fun provideTodayDate(): LocalDate = LocalDate.now()

    @Provides
    @Singleton
    fun provideShiftDatabase(@ApplicationContext context: Context): ShiftDatabase =
        Room.databaseBuilder(context, ShiftDatabase::class.java, ShiftDatabase.DATABASE_NAME).build()

    @Provides
    fun provideScheduleWeekDao(db: ShiftDatabase): ScheduleWeekDao = db.scheduleWeekDao()

    @Provides
    @Singleton
    fun provideScheduleRepository(dao: ScheduleWeekDao): ScheduleRepository =
        ScheduleRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    @Provides
    @Singleton
    fun provideOcrEngine(recognizer: TextRecognizer): OcrEngine =
        MlKitOcrEngine(recognizer)

    @Provides
    @Singleton
    fun provideScheduleParser(): ScheduleParser =
        ScheduleParserImpl(Stage1Validator())

    @Provides
    @Singleton
    fun provideFailedImageReporter(): FailedImageReporter =
        NoOpFailedImageReporter()

    @Provides
    @Suppress("LongParameterList")
    fun provideProcessScheduleImageUseCase(
        ocrEngine: OcrEngine,
        parser: ScheduleParser,
        reporter: FailedImageReporter,
    ): ProcessScheduleImageUseCase =
        ProcessScheduleImageUseCase(ocrEngine, parser, reporter)
}
