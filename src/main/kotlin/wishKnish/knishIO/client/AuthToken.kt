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
  data class Wallet(
    val position: String?,
    val characters: String?,
    val mlkemParameterSet: Int? = null
  )
  inner class Snapshot(
    val token: String,
    val expiresAt: String,
    val pubkey: String,
    val encrypt: Boolean
  ) {
    var wallet: Wallet
    init {
      requireNotNull(getWallet()) { "Wallet not initialised" }
      wallet = Wallet(
        getWallet()!!.position,
        getWallet()!!.characters,
        getWallet()!!.mlkemParameterSet
      )
    }
  }

  companion object {
    @JvmStatic
    fun create(data: AccessToken, wallet: ClientWallet, encrypt: Boolean = false): AuthToken {
      val authToken = AuthToken(data.token, data.expiresAt.toString(), data.encrypt ?: false, data.pubkey)
      authToken.setWallet(wallet)

      return authToken
    }

    /**
     * ML-KEM parameter set a restored session must use, resolved in three tiers: an explicit
     * snapshot field, then the stored validator key's length, then ML-KEM-768.
     *
     * The final tier is deliberately NOT the constructor default. A snapshot with neither an
     * explicit field nor a recognisable key can only have come from a pre-bump build, and every
     * pre-bump build was ML-KEM-768-only — defaulting to 1024 would make the restored wallet
     * advertise a public key the validator never recorded for that token, and would throw on the
     * first outbound encapsulation to the session's stored 1184-byte validator key.
     */
    @JvmStatic
    fun resolveMlkemParameterSet(snapshot: Snapshot): Int {
      return snapshot.wallet.mlkemParameterSet
        ?: ClientWallet.mlkemParameterSetFromPubkey(snapshot.pubkey)
        ?: 768
    }

    @JvmStatic
    fun restore(snapshot: Snapshot, secret: String): AuthToken {
      val wallet = ClientWallet(
        secret,
        "AUTH",
        snapshot.wallet.position,
        null,
        snapshot.wallet.characters,
        resolveMlkemParameterSet(snapshot)
      )

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
