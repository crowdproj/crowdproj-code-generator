import com.crowdproj.generator.api.v1.models.AdCreateRequest
import com.crowdproj.generator.api.v1.models.IRequestAd
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains

class SerializationTest {
    @Test
    fun ser() {
        val req: IRequestAd = AdCreateRequest(
            requestId = "xxx"
        )
        val json = Json.encodeToString(req)
//        val json = Json.encodeToString(AdCreateRequest.serializer(), req)
//        val json = Json.encodeToString(IRequestAd.serializer(), req)
        println(json)
        assertContains(json, "\"requestType\":\"create\"")
    }
}
