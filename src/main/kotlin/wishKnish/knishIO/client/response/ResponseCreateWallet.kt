@file:JvmName("ResponseCreateWallet")
package wishKnish.knishIO.client.response

import wishKnish.knishIO.client.mutation.MutationCreateWallet

class ResponseCreateWallet(
  query: MutationCreateWallet,
  json: String,
): ResponseProposeMolecule(query, json)