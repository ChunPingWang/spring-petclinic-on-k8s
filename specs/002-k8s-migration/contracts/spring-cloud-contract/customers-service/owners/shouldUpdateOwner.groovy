import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should update an existing owner"

    request {
        method PUT()
        url '/owners/1'
        headers {
            contentType(applicationJson())
        }
        body([
            firstName: 'George',
            lastName: 'Franklin',
            address: '200 New Address St.',
            city: 'NewCity',
            telephone: '5559876543'
        ])
    }

    response {
        status NO_CONTENT()
    }
}
