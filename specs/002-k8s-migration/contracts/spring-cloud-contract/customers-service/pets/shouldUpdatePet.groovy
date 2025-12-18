import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should update an existing pet"

    request {
        method PUT()
        url $(consumer(regex('/owners/[0-9]+/pets/[0-9]+')), producer('/owners/1/pets/1'))
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            name: 'Leo Updated',
            birthDate: '2020-01-01',
            typeId: 1
        ])
    }

    response {
        status NO_CONTENT()
    }
}
