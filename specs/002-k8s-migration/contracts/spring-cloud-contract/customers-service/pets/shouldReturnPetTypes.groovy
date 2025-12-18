import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return all pet types"

    request {
        method GET()
        url '/petTypes'
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
                name: $(consumer('cat'), producer(regex('[a-z]+')))
            ],
            [
                id: $(consumer(2), producer(regex('[0-9]+'))),
                name: $(consumer('dog'), producer(regex('[a-z]+')))
            ],
            [
                id: $(consumer(3), producer(regex('[0-9]+'))),
                name: $(consumer('lizard'), producer(regex('[a-z]+')))
            ],
            [
                id: $(consumer(4), producer(regex('[0-9]+'))),
                name: $(consumer('snake'), producer(regex('[a-z]+')))
            ],
            [
                id: $(consumer(5), producer(regex('[0-9]+'))),
                name: $(consumer('bird'), producer(regex('[a-z]+')))
            ],
            [
                id: $(consumer(6), producer(regex('[0-9]+'))),
                name: $(consumer('hamster'), producer(regex('[a-z]+')))
            ]
        ])
    }
}
