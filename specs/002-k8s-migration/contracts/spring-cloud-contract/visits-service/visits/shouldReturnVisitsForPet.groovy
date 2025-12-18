import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return visits for a specific pet"

    request {
        method GET()
        url $(consumer(regex('/owners/[0-9]+/pets/[0-9]+/visits')), producer('/owners/1/pets/1/visits'))
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
            [
                id: $(consumer(1), producer(regex('[0-9]+'))),
                date: $(consumer('2025-12-01'), producer(regex('\\d{4}-\\d{2}-\\d{2}'))),
                description: $(consumer('rabies shot'), producer(regex('.+'))),
                petId: 1
            ],
            [
                id: $(consumer(2), producer(regex('[0-9]+'))),
                date: $(consumer('2025-12-15'), producer(regex('\\d{4}-\\d{2}-\\d{2}'))),
                description: $(consumer('annual checkup'), producer(regex('.+'))),
                petId: 1
            ]
        ])
    }
}
