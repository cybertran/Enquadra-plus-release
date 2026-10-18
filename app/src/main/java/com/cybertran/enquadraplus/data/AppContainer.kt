package com.cybertran.enquadraplus.data

import android.content.Context
import com.cybertran.enquadraplus.data.importer.CtbAssetImporter
import com.cybertran.enquadraplus.data.importer.MbftAssetImporter
import com.cybertran.enquadraplus.data.importer.NormativeAssetImporter
import com.cybertran.enquadraplus.data.importer.NormativeFullTextAssetImporter
import com.cybertran.enquadraplus.data.importer.RelationsAssetImporter
import com.cybertran.enquadraplus.data.local.EnquadraDatabase
import com.cybertran.enquadraplus.data.repository.OfflineCtbRepository
import com.cybertran.enquadraplus.data.repository.OfflineNormativeRepository
import com.cybertran.enquadraplus.data.repository.OfflineTrafficRepository
import com.cybertran.enquadraplus.data.repository.OfflineUserInfractionRepository
import com.cybertran.enquadraplus.data.update.SafeNormativeUpdateRepository
import com.cybertran.enquadraplus.data.user.UserDataDatabase
import com.cybertran.enquadraplus.domain.repository.CtbRepository
import com.cybertran.enquadraplus.domain.repository.NormativeRepository
import com.cybertran.enquadraplus.domain.repository.TrafficRepository
import com.cybertran.enquadraplus.domain.repository.UserInfractionRepository

class AppContainer(context: Context) {

    private val database = EnquadraDatabase.create(context)
    private val userDataDatabase = UserDataDatabase.create(context)
    private val mbftImporter = MbftAssetImporter(context, database)
    private val ctbImporter = CtbAssetImporter(context, database)
    private val normativeImporter = NormativeAssetImporter(context, database)
    private val normativeFullTextImporter =
        NormativeFullTextAssetImporter(context, database)
    private val relationsImporter = RelationsAssetImporter(context, database)

    val trafficRepository: TrafficRepository =
        OfflineTrafficRepository(database.infractionDao())

    val userInfractionRepository: UserInfractionRepository =
        OfflineUserInfractionRepository(userDataDatabase.userInfractionDao())

    val ctbRepository: CtbRepository =
        OfflineCtbRepository(database.ctbDao())

    val normativeRepository: NormativeRepository =
        OfflineNormativeRepository(database.normativeDao())

    val safeUpdateRepository = SafeNormativeUpdateRepository(context, database)

    suspend fun importBundledDatasetIfNeeded() {
        mbftImporter.importIfNeeded()
        relationsImporter.importIfNeeded()
        ctbImporter.importIfNeeded()
        normativeImporter.importIfNeeded()
        normativeFullTextImporter.importIfAvailable()
        safeUpdateRepository.bootstrap()
    }
}
