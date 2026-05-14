package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.system.SystemInformationDTO
import beer.thierry.budgetplanner.api.services.system.ISystemInformationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/system")
@RestController
class SystemInformationResource(private val systemInformationService: ISystemInformationService) {

    @GetMapping
    fun getSystemInformation(): ResponseEntity<SystemInformationDTO> {
        val info = systemInformationService.fetchSystemInformation()
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(info)
    }
}
