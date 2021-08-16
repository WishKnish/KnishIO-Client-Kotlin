@file:JvmName("IResponseRequestAuthorization")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.Wallet

interface IResponseRequestAuthorization {
    fun token(): String
    fun time(): Int
    fun pubKey(): String
    fun encrypt(): Boolean
    fun success(): Boolean
    fun wallet(): Wallet
    fun reason(): String?
}