import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return 404 when owner not found"

    request {
        method GET()
        url '/owners/99999'
        headers {
            accept(applicationJson())
        }
    }

    response {
        status NOT_FOUND()
    }
}
