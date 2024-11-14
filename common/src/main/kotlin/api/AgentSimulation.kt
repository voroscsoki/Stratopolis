package dev.voroscsoki.stratopolis.common.api

class AgentSimulation(val agent: Agent) {
    val agentSpeed = 0.0000001

    fun tick(callback: (Agent) -> Unit) {
        agent.location += (agent.targetBuilding.coords - agent.atBuilding.coords) * (1 / 10.0)
        callback(agent)
        if(agent.location dist agent.targetBuilding.coords < 0.0001) {
            agent.atBuilding = agent.targetBuilding.also { agent.targetBuilding = agent.atBuilding }
        }
    }
}