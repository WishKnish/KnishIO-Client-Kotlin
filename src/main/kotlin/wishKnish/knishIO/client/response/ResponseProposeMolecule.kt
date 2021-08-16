@file:JvmName("ResponseProposeMolecule")
package wishKnish.knishIO.client.response


import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.json.response.mutation.molecule.MoleculeResponse
import wishKnish.knishIO.client.mutation.MutationProposeMolecule
import wishKnish.knishIO.client.data.graphql.types.Molecule as ProposeMolecule


open class ResponseProposeMolecule(
  query: MutationProposeMolecule,
  json: String,
): Response(query, json, "data.ProposeMolecule") {
  var clientMolecule: Molecule? = null

  init {
    clientMolecule = query.molecule()
  }

  override fun mapping(response: String): MoleculeResponse {
    return MoleculeResponse.jsonToObject(response)
  }

  override fun data(): ProposeMolecule {
    return super.data() as ProposeMolecule
  }

  fun clientMolecule(): Molecule? {
    return clientMolecule
  }

  fun molecule(): Molecule {
    val data = data()

    return Molecule().apply {
      molecularHash = data.molecularHash
      status = data.status
      createdAt = data.createdAt!!
    }
  }

  fun reason(): String? {
    return data().reason
  }

  override fun status(): String {
    return data().status as String
  }

  override fun success(): Boolean {
    return status() == "accepted"
  }
}