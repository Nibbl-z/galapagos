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
