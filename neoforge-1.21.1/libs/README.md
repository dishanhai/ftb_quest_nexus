# NeoForge 1.21.1 開發依賴

作者：dishanhai。以下第三方 jar 僅供本機開發與測試，不會展開或內嵌於 Nexus。

| 依賴 | 版本 | 官方下載來源 | SHA256 |
| --- | --- | --- | --- |
| Applied Energistics 2 | 19.2.18 | https://cdn.modrinth.com/data/XxWD5pD3/versions/KDnFUmMm/appliedenergistics2-19.2.18.jar | DF15A07F86CA1CA93AA66373F9D5D84DD3D4586631FCEB38F4FBC85DAD591E39 |
| FTB Quests | 2101.1.36 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-neoforge/2101.1.36/ftb-quests-neoforge-2101.1.36.jar | 6A39982C4B3AFC00B1E788041A4F0D394E1659E3B3A6E5D078CD790BC36D0018 |
| FTB Library | 2101.1.37 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-library-neoforge/2101.1.37/ftb-library-neoforge-2101.1.37.jar | 624C2D2D1CBADB946D967E17757BD5B2D129D4D3ED71F2DA0C70607D04476AC2 |
| FTB Teams | 2101.1.11 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-teams-neoforge/2101.1.11/ftb-teams-neoforge-2101.1.11.jar | E6BCD670493624E6906DF872A9C6AF4A33FE6405CAC57A32C59CD4CD5838C655 |
| Architectury | 13.0.11 | https://cdn.modrinth.com/data/lhGA9TYQ/versions/1IiqEQGl/architectury-13.0.11-neoforge.jar | 9CC92F2C09533FC5482C60F993BD891C655A0E32B637037CDCB7C2B23ADEDEEB |
| GuideME | 21.1.22 | https://cdn.modrinth.com/data/Ck4E7v7R/versions/QeYKsstN/guideme-21.1.22.jar | 108337FF98C706B18ED2FBEC077B3337BA72C08F8B46A273534886280E89EF0D |
| Jade（選用） | 15.10.6 | https://cdn.modrinth.com/data/nvQzSEkH/versions/eYz2YBGT/Jade-1.21.1-NeoForge-15.10.6.jar | 276F1F65A20D04F536989C4F311949D770B52C440C23C2B904A08860F3C93810 |
| Mekanism（執行時選用） | 10.7.19.85 | `Mekanism-1.21.1-10.7.19.85.jar` | 004DBC9F3106F4D192AEAA1EE1190DD16EC9CA8059ED3D093B80034F4C574F43 |
| Applied Mekanistics（執行時選用） | 1.6.3 | `Applied-Mekanistics-1.6.3.jar` | 8946FEA39451DBCE8E709DEDBEF40A52BA337BDF7A25AC0C4B503800B1BF0773 |

編譯需將兩個化學品相容性 jar 放在本目錄。開發時以 `-PmekanismRuntime` 將它們加入 NeoForge 執行環境。

另需獨立 SH-Better-UI NeoForge 1.21.1 1.0.3，以及 Maven 取得的 `thedarkcolour:kotlinforforge-neoforge:5.10.0`（UI 庫所需的外部 runtime）。SH-Better-UI 預設從相鄰專案的 `neoforge/build/libs/` 取得，可由 `-PshBetterUiJar=<path>` 覆寫。

FTBQ 的 sources jar 可由同一 Maven 路徑將檔名改為 `ftb-quests-neoforge-2101.1.36-sources.jar` 取得。sources 不進入 runtime classpath。
