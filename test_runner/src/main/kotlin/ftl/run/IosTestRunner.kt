package ftl.run

import com.google.api.services.testing.model.TestMatrix
import ftl.args.IosArgs
import ftl.config.FtlConstants
import ftl.gc.GcIosMatrix
import ftl.gc.GcIosTestMatrix
import ftl.gc.GcStorage
import ftl.gc.GcToolResults
import ftl.http.executeWithRetry
import ftl.ios.Xctestrun
import ftl.json.MatrixMap
import ftl.run.GenericTestRunner.beforeRunMessage
import ftl.util.ShardCounter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object IosTestRunner {
    // https://github.com/bootstraponline/gcloud_cli/blob/5bcba57e825fc98e690281cf69484b7ba4eb668a/google-cloud-sdk/lib/googlecloudsdk/api_lib/firebase/test/ios/matrix_creator.py#L109
    // https://cloud.google.com/sdk/gcloud/reference/alpha/firebase/test/ios/run
    // https://cloud.google.com/sdk/gcloud/reference/alpha/firebase/test/ios/
    suspend fun runTests(iosArgs: IosArgs): Pair<MatrixMap, List<List<String>>> = coroutineScope {
        val (stopwatch, runGcsPath) = GenericTestRunner.beforeRunTests(iosArgs)

        val iosDeviceList = GcIosMatrix.build(iosArgs.devices)

        val xcTestParsed = Xctestrun.parse(iosArgs.xctestrunFile)

        val jobs = arrayListOf<Deferred<TestMatrix>>()
        val runCount = iosArgs.repeatTests
        val shardCounter = ShardCounter()
        val history = GcToolResults.createToolResultsHistory(iosArgs)

        // Upload only after parsing shards to detect missing methods early.
        val xcTestGcsPath = if (iosArgs.xctestrunZip.startsWith(FtlConstants.GCS_PREFIX)) {
            iosArgs.xctestrunZip
        } else {
            GcStorage.uploadXCTestZip(iosArgs, runGcsPath)
        }

        println(beforeRunMessage(iosArgs, iosArgs.testShardChunks))
        repeat(runCount) {
            iosArgs.testShardChunks.forEach { testTargets ->
                jobs += async(Dispatchers.Default) {
                    GcIosTestMatrix.build(
                        iosDeviceList = iosDeviceList,
                        testZipGcsPath = xcTestGcsPath,
                        runGcsPath = runGcsPath,
                        testTargets = testTargets,
                        xcTestParsed = xcTestParsed,
                        args = iosArgs,
                        shardCounter = shardCounter,
                        toolResultsHistory = history
                    ).executeWithRetry()
                }
            }
        }

        val matrixMap = GenericTestRunner.afterRunTests(jobs.awaitAll(), runGcsPath, stopwatch, iosArgs)
        matrixMap to iosArgs.testShardChunks
    }
}
