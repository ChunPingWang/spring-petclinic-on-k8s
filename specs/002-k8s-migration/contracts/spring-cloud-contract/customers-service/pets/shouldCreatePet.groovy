import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a new pet for owner"

    request {
        method POST()
        url $(consumer(regex('/owners/[0-9]+/pets')), producer('/owners/1/pets'))
        headers {
            contentType(applicationJson())
        }
        body([
            name: 'Buddy',
            birthDate: '2023-05-15',
            typeId: 2
        ])
    }

    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(consumer(1), producer(regex('[0-9]+'))),
            name: fromRequest().body('$.name'),
            birthDate: fromRequest().body('$.birthDate'),
            type: [
                id: $(consumer(2), producer(regex('[0-9]+'))),
                name: $(consumer('dog'), producer(regex('[a-z]+')))
            ]
        ])
    }
}
