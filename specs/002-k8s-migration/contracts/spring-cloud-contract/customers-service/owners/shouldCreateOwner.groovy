import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a new owner"

    request {
        method POST()
        url '/owners'
        headers {
            contentType(applicationJson())
        }
        body([
            firstName: 'John',
            lastName: 'Doe',
            address: '123 Main St',
            city: 'Springfield',
            telephone: '5551234567'
        ])
    }

    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(consumer(1), producer(regex('[0-9]+'))),
            firstName: fromRequest().body('$.firstName'),
            lastName: fromRequest().body('$.lastName'),
            address: fromRequest().body('$.address'),
            city: fromRequest().body('$.city'),
            telephone: fromRequest().body('$.telephone'),
            pets: []
        ])
    }
}
