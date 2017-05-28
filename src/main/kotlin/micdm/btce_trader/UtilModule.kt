package micdm.btce_trader

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class UtilModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
}
