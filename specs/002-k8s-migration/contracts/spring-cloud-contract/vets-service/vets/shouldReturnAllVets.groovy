import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return all vets with their specialties"

    request {
        method GET()
        url '/vets'
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
                firstName: $(consumer('James'), producer(regex('[a-zA-Z]+'))),
                lastName: $(consumer('Carter'), producer(regex('[a-zA-Z]+'))),
                specialties: []
            ],
            [
                id: $(consumer(2), producer(regex('[0-9]+'))),
                firstName: $(consumer('Helen'), producer(regex('[a-zA-Z]+'))),
                lastName: $(consumer('Leary'), producer(regex('[a-zA-Z]+'))),
                specialties: [
                    [
                        id: $(consumer(1), producer(regex('[0-9]+'))),
                        name: $(consumer('radiology'), producer(regex('[a-z]+')))
                    ]
                ]
            ],
            [
                id: $(consumer(3), producer(regex('[0-9]+'))),
                firstName: $(consumer('Linda'), producer(regex('[a-zA-Z]+'))),
                lastName: $(consumer('Douglas'), producer(regex('[a-zA-Z]+'))),
                specialties: [
                    [
                        id: $(consumer(2), producer(regex('[0-9]+'))),
                        name: $(consumer('surgery'), producer(regex('[a-z]+')))
                    ],
                    [
                        id: $(consumer(3), producer(regex('[0-9]+'))),
                        name: $(consumer('dentistry'), producer(regex('[a-z]+')))
                    ]
                ]
            ]
        ])
    }
}
