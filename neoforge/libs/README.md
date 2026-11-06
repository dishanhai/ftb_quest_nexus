# NeoForge 1.21.1 開發依賴

作者：dishanhai。以下第三方 jar 僅供本機開發與測試，不會展開或內嵌於 Nexus。

| 依賴 | 版本 | 官方下載來源 | SHA256 |
| --- | --- | --- | --- |
| Applied Energistics 2 | 19.2.18 | https://cdn.modrinth.com/data/XxWD5pD3/versions/KDnFUmMm/appliedenergistics2-19.2.18.jar | DF15A07F86CA1CA93AA66373F9D5D84DD3D4586631FCEB38F4FBC85DAD591E39 |
| FTB Quests | 2101.1.38 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-neoforge/2101.1.38/ftb-quests-neoforge-2101.1.38.jar | 3752657A82C69B0A80C034700DF86F1A3D7C5F3E3AAD814B5AF748B9082A834B |
| FTB Library | 2101.1.39 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-library-neoforge/2101.1.39/ftb-library-neoforge-2101.1.39.jar | CA64BD1B5F335406900F74EEAFACEA5A3580A3933D61DE6B6600A64EB12A9A95 |
| FTB Teams | 2101.1.12 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-neoforge/2101.1.12/ftb-teams-neoforge-2101.1.12.jar | B3F33436070B7AE4A706280561F1D2F1D1EB1A089CB13EC9851E99646165AED2 |
| Architectury | 13.0.11 | https://cdn.modrinth.com/data/lhGA9TYQ/versions/1IiqEQGl/architectury-13.0.11-neoforge.jar | 9CC92F2C09533FC5482C60F993BD891C655A0E32B637037CDCB7C2B23ADEDEEB |
| GuideME | 21.1.22 | https://cdn.modrinth.com/data/Ck4E7v7R/versions/QeYKsstN/guideme-21.1.22.jar | 108337FF98C706B18ED2FBEC077B3337BA72C08F8B46A273534886280E89EF0D |
| Jade（選用） | 15.10.6 | https://cdn.modrinth.com/data/nvQzSEkH/versions/eYz2YBGT/Jade-1.21.1-NeoForge-15.10.6.jar | 276F1F65A20D04F536989C4F311949D770B52C440C23C2B904A08860F3C93810 |

另需獨立 SH-Better-UI NeoForge 1.21.1 1.0.3，以及 Maven 取得的 `thedarkcolour:kotlinforforge-neoforge:5.10.0`（UI 庫所需的外部 runtime）。SH-Better-UI 預設從相鄰專案的 `neoforge/build/libs/` 取得，可由 `-PshBetterUiJar=<path>` 覆寫。

FTBQ 的 sources jar 可由同一 Maven 路徑將檔名改為 `ftb-quests-neoforge-2101.1.38-sources.jar` 取得。sources 不進入 runtime classpath。
