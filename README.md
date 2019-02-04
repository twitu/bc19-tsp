# bc19-tsp
Battle Code 2019 Bot for The Shadow Priests built with great care and effort by [twitu](https://github.com/twitu), [lsampras](https://github.com/lsampras), [Arkonaire](https://github.com/Arkonaire)

## Strategy
* Economy rush with optimized Pilgrim positioning with depot clustering
* Aggressive approach on mid cluster depots and shield units for vulnerable Church and Castle protection
* Mid game Prophet lattice interspersed with Preacher and Crusader
* End game health farming with Crusader flood.

## Economy rush
* Prioritize mid cluster with Pilgrim and escort Prophet
* Castles to fill own depots
* Castles send explorer pilgrim to capture other depot clusters

## Attack micros
* Prophet - Mark and Attack  
Prophet retreats unless it has two allies adjacent to it. It listens to radio broadcasts for marked enemy units and attacks those. If it does not find any such unit it marks a units radio broadcasts the marks and attacks it.
* Preacher and Crusader - Panther Strike  
Defense units standby until they here an attack a message with a specific location. After the assault they return to there defense positions. Panther strike can be called be called by Prophets, Churches and Castles

## Health Farming
* Water like mechanics for Crusaders to fill up every available space
* Equation to calculate optimum time to begin health farming to fully utilizes all resources by the end of Round 1000
