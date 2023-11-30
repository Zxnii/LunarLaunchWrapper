# LunarLaunchWrapper

LunarLaunchWrapper is a tiny kotlin wrapper to download and launch Lunar from the CLI or from a Minecraft launcher of your choice.

## How to use

To use this, simply start the jar file with the `--module` and `--version` argument as well as the main class `wtf.zani.launchwrapper.LunarLaunchWrapper`

If you want to use this on the PrismMC launcher, you will have to create a new instance with the version of your choice. Then edit that instance and replace the minecraft jar with LunarLaunchWrapper.jar and replace the content of the file by this :
```json
{
    "assetIndex": {
        "id": "1.8",
        "sha1": "f6ad102bcaa53b1a58358f16e376d548d44933ec",
        "size": 78494,
        "totalSize": 114885064,
        "url": "https://piston-meta.mojang.com/v1/packages/f6ad102bcaa53b1a58358f16e376d548d44933ec/1.8.json"
    },
    "formatVersion": 1,
    "libraries": [],
    "mainClass": "wtf.zani.launchwrapper.LunarLaunchWrapperKt",
    "mainJar": {
        "downloads": {
            "artifact": {
                "sha1": "3870888a6c3d349d3771a3e9d16c9bf5e076b908",
                "size": 8461484,
                "url": "https://launcher.mojang.com/v1/objects/3870888a6c3d349d3771a3e9d16c9bf5e076b908/client.jar"
            }
        },
        "name": "com.mojang:minecraft:1.8.9:client"
    },
    "minecraftArguments": "--username ${auth_player_name} --version ${version_name} -module lunar --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userProperties ${user_properties} --userType ${user_type}",
    "name": "Minecraft",
    "releaseTime": "2015-12-03T09:24:39+00:00",
    "type": "release",
    "uid": "net.minecraft",
    "version": "1.8.9"
}
```
Adapt the above for your version.

## Using Weave

If you want to use weave you will have to use a custom version of Weave. You can use the one in the releases :)

Enjoy :)
