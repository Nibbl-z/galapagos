package main

import (
	"bytes"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"sync"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/joho/godotenv"
	"golang.org/x/time/rate"
)

// source: stolen directly from the gin documentation :D https://gin-gonic.com/en/docs/middleware/security-guide/#rate-limiting
func RateLimiter() gin.HandlerFunc {
	type client struct {
		limiter *rate.Limiter
	}

	var (
		mu      sync.Mutex
		clients = make(map[string]*client)
	)

	return func(c *gin.Context) {
		ip := c.ClientIP()

		mu.Lock()
		if _, exists := clients[ip]; !exists {
			clients[ip] = &client{limiter: rate.NewLimiter(1.0 / 60.0, 5)}
		}
		cl := clients[ip]
		mu.Unlock()

		if !cl.limiter.Allow() {
			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
				"errors": "rate limit exceeded",
			})
			return
		}

		c.Next()
	}
}

func main() {
	err := godotenv.Load()
	if err != nil {
		panic("Failed to load .env file!")
	}

	apiKey := os.Getenv("NOXCREW_API_KEY")

	gin.SetMode(gin.ReleaseMode)

	router := gin.Default()

	router.Use(RateLimiter())


	router.GET("/fetch_api/:uuid", func(ctx *gin.Context) {
		providedUUID := ctx.Request.Header.Get("X-MC-UUID")
		id := ctx.Param("uuid")

		if uuid.Validate(id) != nil {
			ctx.JSON(http.StatusBadRequest, gin.H{
				"errors" : "Provided UUID is invalid",
			})
			return
		}

		if (providedUUID != id) {
			ctx.JSON(http.StatusUnauthorized, gin.H{
				"messsage" : "Unauthorized",
			})
			return
		}

		graphQLQuery := fmt.Sprintf(strings.ReplaceAll(strings.ReplaceAll(`
		query fetchPlayerData {
			player(uuid: \"%s\") {
				collections {
                  cosmetics {
                    cosmetic {
                      trophies
                      name
                      collection
                      type
                      isBonusTrophies
                    }
                    chromaPacks
                    owned
                    donationsMade
                  }
                }
                infinibag {
                  amount
                  asset {
                    name
                    ... on CosmeticToken {
                      __typename
                    }
                  }
                }
                infinivault {
                  amount
                  asset {
                    name
                    ... on CosmeticToken {
                      __typename
                    }
                  }
                }
                statistics {
                  battle_box_xp_earned: rotationValue(statisticKey: \"battle_box_xp_earned\")
                  battle_box_quads_xp_earned: rotationValue(statisticKey: \"battle_box_quads_xp_earned\")
                  battle_box_arena_xp_earned: rotationValue(statisticKey: \"battle_box_arena_xp_earned\")
                  dynaball_xp_earned: rotationValue(statisticKey: \"dynaball_xp_earned\")
                  hole_in_the_wall_xp_earned: rotationValue(statisticKey: \"hole_in_the_wall_xp_earned\")
                  pw_xp_earned: rotationValue(statisticKey: \"pw_xp_earned\")
                  pw_survival_xp_earned: rotationValue(statisticKey: \"pw_survival_xp_earned\")
                  pw_solo_xp_earned: rotationValue(statisticKey: \"pw_solo_xp_earned\")
                  rocket_spleef_xp_earned: rotationValue(statisticKey: \"rocket_spleef_xp_earned\")
                  sky_battle_xp_earned: rotationValue(statisticKey: \"sky_battle_xp_earned\")
                  sky_battle_quads_xp_earned: rotationValue(statisticKey: \"sky_battle_quads_xp_earned\")
                  sky_battle_solos_xp_earned: rotationValue(statisticKey: \"sky_battle_solos_xp_earned\")
                  tgttos_xp_earned: rotationValue(statisticKey: \"tgttos_xp_earned\")

                  battle_box_quads_games_played: rotationValue(statisticKey: \"battle_box_quads_games_played\")
                  battle_box_arena_games_played: rotationValue(statisticKey: \"battle_box_arena_games_played\")
                  dynaball_games_played: rotationValue(statisticKey: \"dynaball_games_played\")
                  hole_in_the_wall_games_played: rotationValue(statisticKey: \"hole_in_the_wall_games_played\")
                  pw_survival_games_played: rotationValue(statisticKey: \"pw_survival_games_played\")
                  rocket_spleef_games_played: rotationValue(statisticKey: \"rocket_spleef_games_played\")
                  sky_battle_quads_games_played: rotationValue(statisticKey: \"sky_battle_quads_games_played\")
                  sky_battle_solos_games_played: rotationValue(statisticKey: \"sky_battle_solos_games_played\")
                  tgttos_games_played: rotationValue(statisticKey: \"tgttos_games_played\")
                }
                factions {
                  selected
                  totalExperience
                  name
                }
                ranks
              }
            }
		`, "\n", "\\n "), "\t", ""), id)

		request, err := http.NewRequest("POST", "https://api.mccisland.net/graphql", bytes.NewBuffer([]byte(fmt.Sprintf(`{"query" : "%s"}`, graphQLQuery))))

		if err != nil {
			ctx.JSON(http.StatusInternalServerError, gin.H{
				"errors" : "Failed to create http request: " + err.Error(),
			})
			return
		}

		request.Header.Add("Accept", "application/json")
		request.Header.Add("content-type", "application/json")
		request.Header.Add("X-API-Key", apiKey)
		request.Header.Add("User-Agent", fmt.Sprintf("galapagos-web-server/%s, (discord/@nibbl_z)", id))

		response, err := http.DefaultClient.Do(request)

		if err != nil {
			ctx.JSON(http.StatusInternalServerError, gin.H{
				"errors" : "Failed to create http request: " + err.Error(),
			})
			return
		}

		body, err := io.ReadAll(response.Body)

		if err != nil {
			ctx.JSON(http.StatusInternalServerError, gin.H{
				"errors" : "Failed to decode response: " + err.Error(),
			})
			return
		}

		ctx.String(http.StatusOK, string(body))
	})

	router.Run(":3137")
}
