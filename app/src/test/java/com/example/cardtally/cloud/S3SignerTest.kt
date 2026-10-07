package com.example.cardtally.cloud

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class S3SignerTest {
    // Public AWS documentation example credentials, never a user credential.
    @Test fun matchesAwsPublishedGetObjectVector() {
        val hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val signed = S3Signer.headers("GET", "https://examplebucket.s3.amazonaws.com/test.txt".toHttpUrl(),
            hash, "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "us-east-1", "20130524T000000Z", extra = mapOf("Range" to "bytes=0-9"))
        assertTrue(signed.getValue("Authorization").endsWith("Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41"))
    }
    @Test fun reservedAndUnicodeQueryCharactersAreEncoded() {
        assertEquals("a%20b%2F%2B%2A~", S3Signer.encode("a b/+*~"))
        assertEquals("%E5%9B%BE", S3Signer.encode("图"))
    }
}
