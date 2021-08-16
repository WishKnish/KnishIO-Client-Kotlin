@file:JvmName("MoleculeMutationVariable")
package wishKnish.knishIO.client.data.json.variables

import kotlinx.serialization.Serializable
import wishKnish.knishIO.client.Molecule

@Serializable data class MoleculeMutationVariable(@JvmField val molecule: Molecule): IVariable
