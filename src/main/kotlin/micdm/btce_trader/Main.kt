package micdm.btce_trader

fun main(args: Array<String>) {
    if (System.getenv("USE_LOCAL_DATA") != null) {
        DaggerLocalComponent.builder().build()
            .getRunner().start()

    } else {
        DaggerRemoteComponent.builder().build()
            .getRunner().start()
    }
}
