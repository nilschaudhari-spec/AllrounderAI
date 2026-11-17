using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.WebApi.Services;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    public class DatabaseController : ControllerBase
    {
        private readonly IDatabaseService _databaseService;

        public DatabaseController(IDatabaseService databaseService)
        {
            _databaseService = databaseService;
        }

        [HttpGet("status")]
        public async Task<ActionResult<DatabaseStatusResult>> GetStatus()
        {
            var result = await _databaseService.TestConnectionAsync();
            return Ok(result);
        }

        [HttpPost("init")]
        public async Task<ActionResult<SchemaInitResult>> InitializeDatabase()
        {
            var result = await _databaseService.InitializeSchemaAsync();
            return Ok(result);
        }

        [HttpGet("tables")]
        public async Task<ActionResult<List<string>>> GetTables()
        {
            var tables = await _databaseService.GetExistingTablesAsync();
            return Ok(tables);
        }
    }
}
