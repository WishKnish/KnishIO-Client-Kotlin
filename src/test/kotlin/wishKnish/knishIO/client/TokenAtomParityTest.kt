package wishKnish.knishIO.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.data.graphql.types.TokenUnit
import wishKnish.knishIO.client.libraries.Crypto
import wishKnish.knishIO.client.mutation.MutationCreateToken
import wishKnish.knishIO.client.mutation.MutationRequestTokens
import java.net.URI

/**
 * C, T and V atoms built by createToken, requestTokens and a transfer of served token units hash
 * exactly as the JS reference builds them. Every digest literal below was printed by the JS SDK
 * (dist/client.es.mjs, KNISHIO_KCORE=off) from the same inputs: secret
 * generateSecret('fleet-audit-2026-11'), source wallet USER at 'aa'x32, new wallet positions
 * 'bb'x32, remainder 'cc'x32, createdAt '1700000000000', hashed with Atom.hashAtoms.
 */
class TokenAtomParityTest {

  private val secret = Crypto.generateSecret("fleet-audit-2026-11")
  private val positionA = "aa".repeat(32)
  private val positionB = "bb".repeat(32)
  private val positionC = "cc".repeat(32)
  private val batch = "dd".repeat(32)
  private val createdAt = "1700000000000"

  private class Captured : RuntimeException()

  @BeforeEach
  fun pinPositions() {
    mockkObject(Wallet.Companion)
    every { Wallet.generatePosition(any()) } returns positionB
  }

  @AfterEach
  fun unpinPositions() {
    unmockkObject(Wallet.Companion)
  }

  private fun digest(atoms: List<Atom>): String? = Atom.hashAtoms(atoms.map { it.copy(createdAt = createdAt) })

  private fun userMolecule() = Molecule(secret, Wallet(secret, "USER", positionA), Wallet(secret, "USER", positionC))

  private fun units() = mutableListOf(
    TokenUnit("u1", "Unit 1"),
    TokenUnit.jsonToObject("""{"id":"u2","name":"Unit 2","metas":"{\"color\":\"red\"}"}""")
  )

  /** The C atom createToken builds, rebuilt on the pinned molecule from the arguments it passes on. */
  private fun createTokenAtom(call: (KnishIOClient) -> Unit): Atom {
    var captured: List<Any?> = listOf()
    val client = spyk(KnishIOClient(listOf(URI("http://127.0.0.1:1/graphql"))))
    client.setSecret(secret)
    every { client.createMoleculeMutation(MutationCreateToken::class, any()) } answers {
      mockk<MutationCreateToken>().also {
        every { it.fillMolecule(any(), any(), any()) } answers { captured = args; throw Captured() }
      }
    }
    runCatching { call(client) }.exceptionOrNull().let { check(it is Captured) { "createToken did not reach fillMolecule: $it" } }
    @Suppress("UNCHECKED_CAST")
    val molecule = userMolecule().initTokenCreation(captured[0] as Wallet, captured[1] as Number, captured[2] as MutableList<MetaData>)
    return molecule.atoms.first()
  }

  /** The T atom requestTokens builds, rebuilt on the pinned molecule from the arguments it passes on. */
  private fun requestTokensAtom(call: (KnishIOClient) -> Unit): Atom {
    var captured: List<Any?> = listOf()
    val client = spyk(KnishIOClient(listOf(URI("http://127.0.0.1:1/graphql"))))
    client.setSecret(secret)
    every { client.createMoleculeMutation(MutationRequestTokens::class, any()) } answers {
      mockk<MutationRequestTokens>().also {
        every { it.fillMolecule(any(), any(), any(), any(), any(), any()) } answers { captured = args; throw Captured() }
      }
    }
    runCatching { call(client) }.exceptionOrNull().let { check(it is Captured) { "requestTokens did not reach fillMolecule: $it" } }
    @Suppress("UNCHECKED_CAST")
    val molecule = userMolecule().initTokenRequest(
      captured[0] as String, captured[1] as Number, captured[2] as String?, captured[3] as String?, captured[4] as MutableList<MetaData>, captured[5] as String?
    )
    return molecule.atoms.first()
  }

  @Test
  fun `createToken C atoms equal JS for fungible, stackable and stackable units`() {
    val fungible = createTokenAtom {
      it.createToken("TEST", 1000, mutableListOf(MetaData("name", "Test"), MetaData("fungibility", "fungible")))
    }
    val stackable = createTokenAtom {
      it.createToken("TEST", 1000, mutableListOf(MetaData("name", "Test"), MetaData("fungibility", "stackable")), batch)
    }
    val stackableUnits = createTokenAtom {
      it.createToken("TEST", null, mutableListOf(MetaData("name", "Test"), MetaData("fungibility", "stackable")), batch, units())
    }

    expect {
      that(fungible.batchId).isEqualTo(null)
      that(fungible.meta.map { it.key }).isEqualTo(
        listOf("name", "fungibility", "walletTokenSlug", "walletBundleHash", "walletAddress", "walletPosition", "walletPubkey", "walletCharacters")
      )
      that(digest(listOf(fungible))).isEqualTo("006e5a47e85cg732ee1e191627dc667e533d595cdb6851c6bee1c97d4d510dda")
      that(stackable.batchId).isEqualTo(batch)
      that(digest(listOf(stackable))).isEqualTo("05a4ba8872405b272b91eecd30d597e6920gc13fg2878gf28a3248bd82ecc9a6")
      that(stackableUnits.meta.map { it.key }.take(6))
        .isEqualTo(listOf("name", "fungibility", "batchId", "splittable", "decimals", "tokenUnits"))
      that(digest(listOf(stackableUnits))).isEqualTo("02bde5c6ba369c1bd88ff19f7e8e01g51b45a966c48fb1ebg2fe81d956b5age2")
    }
  }

  @Test
  fun `requestTokens T atoms equal JS for a wallet, a secret and units`() {
    val recipient = Wallet(secret, "TEST", positionB)
    val toWallet = requestTokensAtom { it.requestTokens("TEST", recipient, 1000.0) }
    val toSecret = requestTokensAtom { it.requestTokens("TEST", Crypto.generateSecret("fleet-audit-2026-11-recipient"), 1000) }
    val withUnits = requestTokensAtom { it.requestTokens("TEST", recipient, null, units()) }

    expect {
      that(toWallet.meta.map { it.key to it.value }).isEqualTo(
        listOf("position" to positionB, "bundle" to recipient.bundle, "token" to "TEST", "amount" to "1000")
      )
      that(digest(listOf(toWallet))).isEqualTo("012d2dc0197e0204b3be9cd5a44e0fg7d0829ffa7de39570g66c5c61b08e90dg")
      that(toSecret.meta.map { it.key }).isEqualTo(listOf("position", "bundle", "token", "amount"))
      that(digest(listOf(toSecret))).isEqualTo("00c9d5g9g2800669753b374184f63f1cb6ba45e3afa7b0152a3e5338gfa534ab")
      that(withUnits.meta.map { it.key }).isEqualTo(listOf("tokenUnits", "position", "bundle", "token", "amount"))
      that(withUnits.meta.first().value)
        .isEqualTo("""[{"id":"u1","name":"Unit 1","metas":{}},{"id":"u2","name":"Unit 2","metas":{"color":"red"}}]""")
      that(digest(listOf(withUnits))).isEqualTo("0396dgbg7af017929c89cgc135a1e2f5bc165c966g113bb2253a5045cde6f790")
    }
  }

  @Test
  fun `served unit metas survive into a transfer as JS sends them`() {
    val served = TokenUnit.jsonToObject("""{"id":"u1","name":"Unit 1","metas":"{\"color\":\"red\",\"fragmentZone\":2}"}""")
    expectThat(Wallet.tokenUnitsJson(listOf(served))).isEqualTo("""[["u1","Unit 1",{"color":"red","fragmentZone":2}]]""")

    val source = Wallet(secret, "TEST", positionA).apply {
      tokenUnits = mutableListOf(served, TokenUnit.jsonToObject("""{"id":"u2","name":"Unit 2","metas":null}"""))
      balance = 2.0
    }
    val recipient = Wallet(secret, "TEST", positionB)
    val remainder = Wallet(secret, "TEST", positionC)
    source.splitUnits(listOf(served), remainder, recipient)
    val molecule = Molecule(secret, source, remainder).initValue(recipient, 1)

    expectThat(digest(molecule.atoms)).isEqualTo("0184b48c9937f195e483f3dede0093a97bcd1755b2gb3bdaea439220cg191172")
  }
}
