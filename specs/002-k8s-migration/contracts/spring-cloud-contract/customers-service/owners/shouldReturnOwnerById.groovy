import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return owner by ID"

    request {
        method GET()
        url '/owners/1'
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
            firstName: $(consumer('George'), producer(regex('[a-zA-Z]+'))),
            lastName: $(consumer('Franklin'), producer(regex('[a-zA-Z]+'))),
            address: $(consumer('110 W. Liberty St.'), producer(regex('.+'))),
            city: $(consumer('Madison'), producer(regex('[a-zA-Z ]+'))),
            telephone: $(consumer('6085551023'), producer(regex('[0-9]{10}'))),
            pets: []
        ])
    }
}
