import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a visit for a pet"

    request {
        method POST()
        url $(consumer(regex('/owners/[0-9]+/pets/[0-9]+/visits')), producer('/owners/1/pets/1/visits'))
        headers {
            contentType(applicationJson())
        }
        body([
            date: $(consumer('2025-12-19'), producer(regex('\\d{4}-\\d{2}-\\d{2}'))),
            description: $(consumer('annual checkup'), producer(regex('.+')))
        ])
    }

    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(consumer(1), producer(regex('[0-9]+'))),
            date: fromRequest().body('$.date'),
            description: fromRequest().body('$.description'),
            petId: 1
        ])
    }
}
