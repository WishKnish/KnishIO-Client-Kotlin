@file:JvmName("MutationProposeMolecule")

package wishKnish.knishIO.client.mutation

import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.data.json.query.MoleculeMutation
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.MoleculeMutationVariable
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.response.IResponse
import wishKnish.knishIO.client.response.ResponseProposeMolecule

open class MutationProposeMolecule @JvmOverloads constructor(
  httpClient: HttpClient,
  open val molecule: Molecule? = null
) : Mutation(httpClient) {
  var remainderWallet: Wallet? = null

  fun molecule(): Molecule? {
    return molecule
  }

  fun remainderWallet(): Wallet? {
    return remainderWallet
  }

  override fun createResponse(json: String): ResponseProposeMolecule {
    return ResponseProposeMolecule(this, json)
  }

  override fun getQuery(variables: IVariable): QueryInterface {
    return MoleculeMutation(variables as MoleculeMutationVariable)
  }

  fun execute(variables: MoleculeMutationVariable): IResponse {
    request = createQuery(variables)

    val resp = client.mutate(request !!)

    response = createResponseRaw(resp)

    return response !!
  }
}
