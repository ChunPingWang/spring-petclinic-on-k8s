import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return visits for multiple pets (batch query)"

    request {
        method GET()
        url '/pets/visits'
        urlPath('/pets/visits') {
            queryParameters {
                parameter 'petId': '1,2,3'
            }
        }
        headers {
            accept(applicationJson())
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            items: [
                [
                    id: $(consumer(1), producer(regex('[0-9]+'))),
                    date: $(consumer('2025-12-01'), producer(regex('\\d{4}-\\d{2}-\\d{2}'))),
                    description: $(consumer('rabies shot'), producer(regex('.+'))),
                    petId: 1
                ],
                [
                    id: $(consumer(2), producer(regex('[0-9]+'))),
                    date: $(consumer('2025-12-15'), producer(regex('\\d{4}-\\d{2}-\\d{2}'))),
                    description: $(consumer('neutering'), producer(regex('.+'))),
                    petId: 2
                ]
            ]
        ])
    }
}
