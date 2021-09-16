/*
                               (
                              (/(
                              (//(
                              (///(
                             (/////(
                             (//////(                          )
                            (////////(                        (/)
                            (////////(                       (///)
                           (//////////(                      (////)
                           (//////////(                     (//////)
                          (////////////(                    (///////)
                         (/////////////(                   (/////////)
                        (//////////////(                  (///////////)
                        (///////////////(                (/////////////)
                       (////////////////(               (//////////////)
                      (((((((((((((((((((              (((((((((((((((
                     (((((((((((((((((((              ((((((((((((((
                     (((((((((((((((((((            ((((((((((((((
                    ((((((((((((((((((((           (((((((((((((
                    ((((((((((((((((((((          ((((((((((((
                    (((((((((((((((((((         ((((((((((((
                    (((((((((((((((((((        ((((((((((
                    ((((((((((((((((((/      (((((((((
                    ((((((((((((((((((     ((((((((
                    (((((((((((((((((    (((((((
                   ((((((((((((((((((  (((((
                   #################  ##
                   ################  #
                  ################# ##
                 %################  ###
                 ###############(   ####
                ###############      ####
               ###############       ######
              %#############(        (#######
             %#############           #########
            ############(              ##########
           ###########                  #############
          #########                      ##############
        %######

        Powered by Knish.IO: Connecting a Decentralized World

Please visit https://github.com/WishKnish/KnishIO-Client-Kotlin for information.

License: https://github.com/WishKnish/KnishIO-Client-Kotlin/blob/master/LICENSE
*/
@file:JvmName("AuthToken")

package wishKnish.knishIO.client

import wishKnish.knishIO.client.data.ClientTokenData
import wishKnish.knishIO.client.data.graphql.types.AccessToken
import wishKnish.knishIO.client.libraries.Strings
import java.math.BigInteger
import wishKnish.knishIO.client.Wallet as ClientWallet

class AuthToken(
 private val token: String,
 private val expiresAt: String,
 private val encrypt: Boolean,
 private val pubkey: String
  ) {
  private var wallet: ClientWallet? = null
  data class Wallet(val position: String?, val characters: String?)
  inner class Snapshot(
    val token: String,
    val expiresAt: String,
    val pubkey: String,
    val encrypt: Boolean
  ) {
    var wallet: Wallet
    init {
      requireNotNull(getWallet()) { "Wallet not initialised" }
      wallet = Wallet(getWallet()!!.position, getWallet()!!.characters)
    }
  }

  companion object {
    @JvmStatic
    fun create(data: AccessToken, wallet: ClientWallet, encrypt: Boolean = false): AuthToken {
      val authToken = AuthToken(data.token, data.expiresAt.toString(), data.encrypt ?: false, data.pubkey)
      authToken.setWallet(wallet)

      return authToken
    }

    @JvmStatic
    fun restore(snapshot: Snapshot, secret: String): AuthToken {
      val wallet = ClientWallet(secret, "AUTH", snapshot.wallet.position, null, snapshot.wallet.characters)

      return create(
        AccessToken(
          snapshot.token,
          snapshot.expiresAt.toInt(),
          snapshot.pubkey,
          snapshot.pubkey,
          snapshot.encrypt,
          snapshot.expiresAt.toInt()
        ),
        wallet
      )
    }
  }

  fun getSnapshot(): Snapshot {
    return Snapshot(
      this.token,
      this.expiresAt,
      this.pubkey,
      this.encrypt
    )
  }

  fun setWallet(wallet: ClientWallet) {
    this.wallet = wallet
  }

  fun getWallet(): ClientWallet? {
    return wallet
  }

  fun getToken(): String {
    return token
  }

  fun getPubkey(): String {
    return pubkey
  }

  fun getExpireInterval(): BigInteger {
    return (expiresAt.toBigInteger() * 1000.toBigInteger()) - Strings.currentTimeMillis().toBigInteger()
  }

  fun isExpired(): Boolean {
    return getExpireInterval() < 0.toBigInteger()
  }

  fun getAuthData(): ClientTokenData {
    requireNotNull(getWallet()) { "Wallet not initialised" }
    return ClientTokenData(getToken(), getPubkey(), getWallet()!!)
  }
}
