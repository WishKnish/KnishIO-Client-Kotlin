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
@file:JvmName("Query")

package wishKnish.knishIO.client.query

import wishKnish.knishIO.client.exception.CodeException
import wishKnish.knishIO.client.httpClient.HttpClient
import wishKnish.knishIO.client.data.json.query.QueryInterface
import wishKnish.knishIO.client.data.json.variables.IVariable
import wishKnish.knishIO.client.response.IResponse


abstract class Query(httpClient: HttpClient) : IQuery {
  @JvmField val client = httpClient
  @JvmField var variables: IVariable? = null
  @JvmField var query: QueryInterface? = null
  @JvmField var request: QueryInterface? = null
  @JvmField var response: IResponse? = null

  fun createResponseRaw(response: String) = createResponse(response)

  fun createQuery(variable: IVariable): QueryInterface {
    variables = variable
    query = getQuery(variable)

    if (uri().isEmpty()) {
      throw CodeException("Query::createQuery() - Node URI was not initialized for this client instance!")
    }
    if (query == null) {
      throw CodeException("Query::createQuery() - GraphQL query was not initialized!")
    }

    return query as QueryInterface
  }

  open fun execute(variables: IVariable): IResponse {
    request = createQuery(variables)

    val resp = client.query(request !!)

    response = createResponseRaw(resp)

    return response !!
  }

  fun uri(): String = client.uri.toASCIIString()
  fun variables() = variables
}
