import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return pet by ID"

    request {
        method GET()
        url $(consumer(regex('/owners/[0-9]+/pets/[0-9]+')), producer('/owners/1/pets/1'))
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
            id: $(consumer(1), producer(regex('[0-9]+'))),
            name: $(consumer('Leo'), producer(regex('[a-zA-Z]+'))),
            birthDate: $(consumer('2020-01-01'), producer(regex('\\d{4}-\\d{2}-\\d{2}'))),
            type: [
                id: $(consumer(1), producer(regex('[0-9]+'))),
                name: $(consumer('cat'), producer(regex('[a-z]+')))
            ]
        ])
    }
}
