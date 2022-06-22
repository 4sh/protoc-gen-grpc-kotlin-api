package com.test.rpc

import com.test.ResultApiDef
import com.test.SearchRequestApiDef
import com.test.SearchResultApiDef
import com.test.SearchServiceDef
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties

class ApiCompilerTest {

    @Test
    fun `should properly generate api files from proto following standard case`() {

        expectThat(SearchRequestApiDef::from.returnType.isMarkedNullable).isEqualTo(true)
        expectThat(SearchRequestApiDef::to.returnType.isMarkedNullable).isEqualTo(true)
        expectThat(SearchRequestApiDef::resultLimit.returnType.isMarkedNullable).isEqualTo(false)
        expectThat(SearchRequestApiDef::resultOffset.returnType.isMarkedNullable).isEqualTo(false)
        expectThat(SearchRequestApiDef::tags.returnType.isMarkedNullable).isEqualTo(true)
        expectThat(SearchRequestApiDef::roles.returnType.isMarkedNullable).isEqualTo(true)

        expectThat(SearchResultApiDef::results.returnType.isMarkedNullable).isEqualTo(true)
        expectThat(SearchResultApiDef::resultOffset.returnType.isMarkedNullable).isEqualTo(false)
        expectThat(SearchResultApiDef::resultLimit.returnType.isMarkedNullable).isEqualTo(false)
        expectThat(SearchResultApiDef::resultTotalCount.returnType.isMarkedNullable).isEqualTo(false)

        expectThat(ResultApiDef::key.returnType.isMarkedNullable).isEqualTo(false)
        expectThat(ResultApiDef::tags.returnType.isMarkedNullable).isEqualTo(true)
        expectThat(ResultApiDef::roles.returnType.isMarkedNullable).isEqualTo(true)

        expectThat(SearchServiceDef::class.declaredFunctions) {
            hasSize(1)
            first()
                .and {
                    val properties = SearchRequestApiDef::class.declaredMemberProperties
                        .map {
                            AbstractParameter(
                                it.name,
                                it.returnType
                            )
                        }
                    get { parameters }
                        .get {
                            filter { it.name != null }
                                .map { AbstractParameter(it.name!!, it.type) }
                        }
                        .containsExactlyInAnyOrder(properties)
                }
                .and {
                    get { returnType }.get { isMarkedNullable }.isEqualTo(false)
                }
                .and {
                    get { returnType }.get { classifier }.and { isA<KClass<SearchResultApiDef>>() }
                }
        }
    }
}

private data class AbstractParameter(
    val name: String,
    val returnType: KType
)
